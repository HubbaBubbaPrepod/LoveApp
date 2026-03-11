import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayInit,
  OnGatewayConnection,
  OnGatewayDisconnect,
  ConnectedSocket,
  MessageBody,
} from '@nestjs/websockets';
import { Logger } from '@nestjs/common';
import { Server, Socket } from 'socket.io';
import { JwtService } from '@nestjs/jwt';
import { DataSource } from 'typeorm';
import { CoupleService } from '../shared/couple.service';
import { MetricsService } from '../metrics/metrics.service';
import { RedisService } from '../redis/redis.service';

interface AuthenticatedSocket extends Socket {
  userId?: number;
  coupleKey?: string;
}

const ENTITY_MAP: Record<string, { table: string; columns: string[] }> = {
  note: {
    table: 'notes',
    columns: ['id', 'user_id', 'couple_key', 'title', 'content', 'color', 'is_pinned', 'sort_order', 'created_at', 'updated_at', 'is_deleted'],
  },
  wish: {
    table: 'wishes',
    columns: ['id', 'user_id', 'couple_key', 'title', 'description', 'is_completed', 'category', 'priority', 'sort_order', 'created_at', 'updated_at', 'is_deleted'],
  },
  mood: {
    table: 'mood_entries',
    columns: ['id', 'user_id', 'couple_key', 'mood', 'note', 'energy_level', 'created_at', 'updated_at', 'is_deleted'],
  },
  activity: {
    table: 'activity_logs',
    columns: ['id', 'user_id', 'couple_key', 'activity_type', 'title', 'description', 'duration', 'date', 'rating', 'created_at', 'updated_at', 'is_deleted'],
  },
  cycle: {
    table: 'menstrual_cycles',
    columns: ['id', 'user_id', 'couple_key', 'cycle_start_date', 'end_date', 'cycle_duration', 'period_duration', 'notes', 'symptoms', 'created_at', 'updated_at', 'is_deleted'],
  },
  calendar: {
    table: 'custom_calendars',
    columns: ['id', 'user_id', 'couple_key', 'name', 'color', 'is_shared', 'created_at', 'updated_at', 'is_deleted'],
  },
  event: {
    table: 'custom_calendar_events',
    columns: ['id', 'user_id', 'couple_key', 'calendar_id', 'title', 'description', 'start_date', 'end_date', 'is_all_day', 'reminder_minutes', 'recurrence_rule', 'created_at', 'updated_at', 'is_deleted'],
  },
  relationship: {
    table: 'relationship_info',
    columns: ['id', 'user_id', 'partner_user_id', 'couple_key', 'anniversary_date', 'relationship_start_date', 'created_at', 'updated_at'],
  },
  chat_message: {
    table: 'chat_messages',
    columns: ['id', 'sender_id', 'couple_key', 'message_type', 'content', 'media_url', 'reply_to_id', 'is_read', 'created_at', 'updated_at', 'is_deleted'],
  },
  memorial: {
    table: 'memorial_days',
    columns: ['id', 'user_id', 'couple_key', 'title', 'date', 'type', 'description', 'notify', 'created_at', 'updated_at', 'is_deleted'],
  },
  spark: {
    table: 'spark_logs',
    columns: ['id', 'user_id', 'couple_key', 'action', 'points', 'created_at'],
  },
  couple_task: {
    table: 'couple_tasks',
    columns: ['id', 'user_id', 'couple_key', 'title', 'description', 'assigned_to', 'is_completed', 'due_date', 'category', 'priority', 'created_at', 'updated_at', 'is_deleted'],
  },
  sleep_entry: {
    table: 'sleep_entries',
    columns: ['id', 'user_id', 'couple_key', 'sleep_time', 'wake_time', 'quality', 'notes', 'created_at', 'updated_at', 'is_deleted'],
  },
  gallery_photo: {
    table: 'gallery_photos',
    columns: ['id', 'user_id', 'couple_key', 'url', 'thumbnail_url', 'caption', 'album_id', 'created_at', 'updated_at', 'is_deleted'],
  },
  miss_you: {
    table: 'miss_you_events',
    columns: ['id', 'user_id', 'couple_key', 'message', 'created_at'],
  },
};

@WebSocketGateway({ cors: { origin: '*' } })
export class SyncGateway
  implements OnGatewayInit, OnGatewayConnection, OnGatewayDisconnect
{
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(SyncGateway.name);

  constructor(
    private readonly jwtService: JwtService,
    private readonly coupleService: CoupleService,
    private readonly dataSource: DataSource,
    private readonly metricsService: MetricsService,
    private readonly redis: RedisService,
  ) {}

  afterInit(server: Server) {
    this.coupleService.setServer(server);
    this.logger.log('SyncGateway initialized');
  }

  async handleConnection(client: AuthenticatedSocket) {
    try {
      const token =
        client.handshake.auth?.token ||
        client.handshake.query?.token;

      if (!token) {
        client.disconnect();
        return;
      }

      const payload = this.jwtService.verify(String(token));
      const userId = payload.userId || payload.sub;
      if (!userId) {
        client.disconnect();
        return;
      }

      client.userId = userId;
      const coupleKey = await this.coupleService.getCoupleKey(userId);
      client.coupleKey = coupleKey;

      client.join(`couple:${coupleKey}`);
      client.join(`user:${userId}`);

      this.metricsService.activeSocketConnections.inc();
      this.logger.log(`Client connected: userId=${userId}, coupleKey=${coupleKey}`);
    } catch {
      client.disconnect();
    }
  }

  handleDisconnect(client: AuthenticatedSocket) {
    if (client.userId) {
      this.metricsService.activeSocketConnections.dec();
      this.logger.log(`Client disconnected: userId=${client.userId}`);
    }
  }

  @SubscribeMessage('data-change')
  async handleDataChange(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody()
    body: {
      entityType: string;
      action: string;
      data?: any;
      clientTimestamp?: string;
    },
  ) {
    if (!client.userId || !client.coupleKey) return;

    const { entityType, action, data, clientTimestamp } = body;
    if (!ENTITY_MAP[entityType]) return;

    const payload = {
      entityType,
      action,
      data,
      senderId: client.userId,
      clientTimestamp,
      serverTimestamp: new Date().toISOString(),
    };

    client
      .to(`couple:${client.coupleKey}`)
      .emit('data-change', payload);

    await this.redis.publish('loveapp:data-changes', {
      coupleKey: client.coupleKey,
      ...payload,
    });

    return { status: 'ok', serverTimestamp: payload.serverTimestamp };
  }

  @SubscribeMessage('sync-request')
  async handleSyncRequest(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() body: { sinceTimestamp?: string },
  ) {
    if (!client.userId || !client.coupleKey) return;

    const since = body.sinceTimestamp || new Date(0).toISOString();
    const changes: Record<string, any[]> = {};

    for (const [entityType, config] of Object.entries(ENTITY_MAP)) {
      const hasCoupleKey = config.columns.includes('couple_key');
      const hasUserId = config.columns.includes('user_id');
      const hasUpdatedAt = config.columns.includes('updated_at');
      const hasCreatedAt = config.columns.includes('created_at');
      const timeCol = hasUpdatedAt ? 'updated_at' : 'created_at';

      let query: string;
      let params: any[];

      if (hasCoupleKey) {
        query = `SELECT * FROM ${config.table} WHERE couple_key = $1 AND ${timeCol} > $2 ORDER BY ${timeCol} ASC LIMIT 500`;
        params = [client.coupleKey, since];
      } else if (hasUserId) {
        query = `SELECT * FROM ${config.table} WHERE user_id = $1 AND ${timeCol} > $2 ORDER BY ${timeCol} ASC LIMIT 500`;
        params = [client.userId, since];
      } else {
        continue;
      }

      try {
        const rows = await this.dataSource.query(query, params);
        if (rows.length) changes[entityType] = rows;
      } catch (err) {
        this.logger.warn(`Sync query failed for ${entityType}: ${err.message}`);
      }
    }

    client.emit('sync-response', {
      changes,
      serverTimestamp: new Date().toISOString(),
    });
  }

  @SubscribeMessage('join-canvas')
  handleJoinCanvas(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() body: { canvasId: string },
  ) {
    if (!client.userId || !body.canvasId) return;
    client.join(`canvas:${body.canvasId}`);
    return { status: 'joined', canvasId: body.canvasId };
  }

  @SubscribeMessage('leave-canvas')
  handleLeaveCanvas(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() body: { canvasId: string },
  ) {
    if (!client.userId || !body.canvasId) return;
    client.leave(`canvas:${body.canvasId}`);
    return { status: 'left', canvasId: body.canvasId };
  }

  @SubscribeMessage('draw-action')
  handleDrawAction(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() body: { canvasId: string; action: any },
  ) {
    if (!client.userId || !body.canvasId) return;
    client.to(`canvas:${body.canvasId}`).emit('draw-action', {
      userId: client.userId,
      action: body.action,
      timestamp: new Date().toISOString(),
    });
  }
}
