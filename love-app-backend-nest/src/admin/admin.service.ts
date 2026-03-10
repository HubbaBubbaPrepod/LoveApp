import {
  Injectable,
  UnauthorizedException,
  NotFoundException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { DataSource } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { AdminLoginDto } from './dto/admin-login.dto';
import { AdminUpdateUserDto } from './dto/admin-update-user.dto';

@Injectable()
export class AdminService {
  constructor(
    private readonly dataSource: DataSource,
    private readonly jwtService: JwtService,
    private readonly config: ConfigService,
  ) {}

  async login(dto: AdminLoginDto) {
    const user = await this.dataSource.query(
      `SELECT id, username, email, password_hash, role, display_name
       FROM users
       WHERE (username = $1 OR email = $1)
       LIMIT 1`,
      [dto.username],
    );

    if (!user.length) throw new UnauthorizedException('Invalid credentials');

    const u = user[0];
    if (!['admin', 'superadmin'].includes(u.role)) {
      throw new UnauthorizedException('Not an admin account');
    }

    const valid = await bcrypt.compare(dto.password, u.password_hash);
    if (!valid) throw new UnauthorizedException('Invalid credentials');

    const secret =
      this.config.get<string>('ADMIN_JWT_SECRET') ||
      this.config.get<string>('JWT_SECRET') + '_admin_panel';

    const token = this.jwtService.sign(
      { userId: u.id, role: u.role, username: u.username },
      { secret, expiresIn: '12h' },
    );

    return { token, user: { id: u.id, username: u.username, role: u.role, display_name: u.display_name } };
  }

  async getStats() {
    const [users, activities, moods, notes, wishes, relationships] =
      await Promise.all([
        this.dataSource.query('SELECT COUNT(*)::int AS count FROM users'),
        this.dataSource.query('SELECT COUNT(*)::int AS count FROM activity_logs'),
        this.dataSource.query('SELECT COUNT(*)::int AS count FROM mood_entries'),
        this.dataSource.query('SELECT COUNT(*)::int AS count FROM notes'),
        this.dataSource.query('SELECT COUNT(*)::int AS count FROM wishes'),
        this.dataSource.query('SELECT COUNT(*)::int AS count FROM relationship_info'),
      ]);

    return {
      users: users[0].count,
      activities: activities[0].count,
      moods: moods[0].count,
      notes: notes[0].count,
      wishes: wishes[0].count,
      relationships: relationships[0].count,
    };
  }

  async getTimeline(days = 30) {
    const rows = await this.dataSource.query(
      `SELECT DATE(created_at) AS date, COUNT(*)::int AS count
       FROM users
       WHERE created_at >= NOW() - $1::int * INTERVAL '1 day'
       GROUP BY DATE(created_at)
       ORDER BY date`,
      [days],
    );
    return rows;
  }

  async getActivitiesByType() {
    const rows = await this.dataSource.query(
      `SELECT activity_type, COUNT(*)::int AS count
       FROM activity_logs
       GROUP BY activity_type
       ORDER BY count DESC
       LIMIT 10`,
    );
    return rows;
  }

  async getUsers(query: {
    _start?: number;
    _end?: number;
    _sort?: string;
    _order?: string;
    q?: string;
  }) {
    const start = query._start ?? 0;
    const end = query._end ?? 25;
    const limit = end - start;
    const sort = query._sort || 'id';
    const order = (query._order || 'ASC').toUpperCase() === 'DESC' ? 'DESC' : 'ASC';

    const allowedSortColumns = ['id', 'username', 'email', 'display_name', 'role', 'created_at', 'is_premium'];
    const sortCol = allowedSortColumns.includes(sort) ? sort : 'id';

    let whereClause = '';
    const params: any[] = [];

    if (query.q) {
      params.push(`%${query.q}%`);
      whereClause = `WHERE u.username ILIKE $1 OR u.email ILIKE $1 OR u.display_name ILIKE $1`;
    }

    const countResult = await this.dataSource.query(
      `SELECT COUNT(*)::int AS total FROM users u ${whereClause}`,
      params,
    );
    const total = countResult[0].total;

    const dataParams = [...params, limit, start];
    const rows = await this.dataSource.query(
      `SELECT u.id, u.username, u.email, u.display_name, u.role, u.is_premium,
              u.profile_image, u.created_at, u.updated_at,
              r.partner_id
       FROM users u
       LEFT JOIN relationship_info r ON r.user_id = u.id
       ${whereClause}
       ORDER BY u.${sortCol} ${order}
       LIMIT $${params.length + 1} OFFSET $${params.length + 2}`,
      dataParams,
    );

    return { data: rows, total };
  }

  async getUser(id: number) {
    const rows = await this.dataSource.query(
      `SELECT u.*, r.partner_id, r.couple_name, r.anniversary_date
       FROM users u
       LEFT JOIN relationship_info r ON r.user_id = u.id
       WHERE u.id = $1`,
      [id],
    );
    if (!rows.length) throw new NotFoundException('User not found');
    return rows[0];
  }

  async updateUser(id: number, dto: AdminUpdateUserDto) {
    const sets: string[] = [];
    const params: any[] = [];
    let idx = 1;

    if (dto.display_name !== undefined) {
      sets.push(`display_name = $${idx++}`);
      params.push(dto.display_name);
    }
    if (dto.email !== undefined) {
      sets.push(`email = $${idx++}`);
      params.push(dto.email);
    }
    if (dto.role !== undefined) {
      sets.push(`role = $${idx++}`);
      params.push(dto.role);
    }

    if (!sets.length) throw new NotFoundException('Nothing to update');

    params.push(id);
    await this.dataSource.query(
      `UPDATE users SET ${sets.join(', ')}, updated_at = NOW() WHERE id = $${idx}`,
      params,
    );

    return this.getUser(id);
  }

  async deleteUser(id: number) {
    const result = await this.dataSource.query(
      'DELETE FROM users WHERE id = $1 RETURNING id',
      [id],
    );
    if (!result.length) throw new NotFoundException('User not found');
    return { deleted: true, id };
  }

  // Activities CRUD
  async getActivities(query: { _start?: number; _end?: number; _sort?: string; _order?: string }) {
    const start = query._start ?? 0;
    const end = query._end ?? 25;
    const limit = end - start;
    const sort = query._sort || 'id';
    const order = (query._order || 'DESC').toUpperCase() === 'ASC' ? 'ASC' : 'DESC';

    const allowedCols = ['id', 'user_id', 'activity_type', 'created_at'];
    const sortCol = allowedCols.includes(sort) ? sort : 'id';

    const countResult = await this.dataSource.query('SELECT COUNT(*)::int AS total FROM activity_logs');
    const total = countResult[0].total;

    const rows = await this.dataSource.query(
      `SELECT * FROM activity_logs ORDER BY ${sortCol} ${order} LIMIT $1 OFFSET $2`,
      [limit, start],
    );
    return { data: rows, total };
  }

  async deleteActivity(id: number) {
    const result = await this.dataSource.query(
      'DELETE FROM activity_logs WHERE id = $1 RETURNING id',
      [id],
    );
    if (!result.length) throw new NotFoundException('Activity not found');
    return { deleted: true, id };
  }

  // Moods CRUD
  async getMoods(query: { _start?: number; _end?: number; _sort?: string; _order?: string }) {
    const start = query._start ?? 0;
    const end = query._end ?? 25;
    const limit = end - start;
    const sort = query._sort || 'id';
    const order = (query._order || 'DESC').toUpperCase() === 'ASC' ? 'ASC' : 'DESC';

    const allowedCols = ['id', 'user_id', 'mood', 'created_at'];
    const sortCol = allowedCols.includes(sort) ? sort : 'id';

    const countResult = await this.dataSource.query('SELECT COUNT(*)::int AS total FROM mood_entries');
    const total = countResult[0].total;

    const rows = await this.dataSource.query(
      `SELECT * FROM mood_entries ORDER BY ${sortCol} ${order} LIMIT $1 OFFSET $2`,
      [limit, start],
    );
    return { data: rows, total };
  }

  async deleteMood(id: number) {
    const result = await this.dataSource.query(
      'DELETE FROM mood_entries WHERE id = $1 RETURNING id',
      [id],
    );
    if (!result.length) throw new NotFoundException('Mood entry not found');
    return { deleted: true, id };
  }

  // Notes CRUD
  async getNotes(query: { _start?: number; _end?: number; _sort?: string; _order?: string }) {
    const start = query._start ?? 0;
    const end = query._end ?? 25;
    const limit = end - start;
    const sort = query._sort || 'id';
    const order = (query._order || 'DESC').toUpperCase() === 'ASC' ? 'ASC' : 'DESC';

    const allowedCols = ['id', 'user_id', 'title', 'created_at'];
    const sortCol = allowedCols.includes(sort) ? sort : 'id';

    const countResult = await this.dataSource.query('SELECT COUNT(*)::int AS total FROM notes');
    const total = countResult[0].total;

    const rows = await this.dataSource.query(
      `SELECT * FROM notes ORDER BY ${sortCol} ${order} LIMIT $1 OFFSET $2`,
      [limit, start],
    );
    return { data: rows, total };
  }

  async deleteNote(id: number) {
    const result = await this.dataSource.query(
      'DELETE FROM notes WHERE id = $1 RETURNING id',
      [id],
    );
    if (!result.length) throw new NotFoundException('Note not found');
    return { deleted: true, id };
  }

  // Wishes CRUD
  async getWishes(query: { _start?: number; _end?: number; _sort?: string; _order?: string }) {
    const start = query._start ?? 0;
    const end = query._end ?? 25;
    const limit = end - start;
    const sort = query._sort || 'id';
    const order = (query._order || 'DESC').toUpperCase() === 'ASC' ? 'ASC' : 'DESC';

    const allowedCols = ['id', 'user_id', 'title', 'created_at'];
    const sortCol = allowedCols.includes(sort) ? sort : 'id';

    const countResult = await this.dataSource.query('SELECT COUNT(*)::int AS total FROM wishes');
    const total = countResult[0].total;

    const rows = await this.dataSource.query(
      `SELECT * FROM wishes ORDER BY ${sortCol} ${order} LIMIT $1 OFFSET $2`,
      [limit, start],
    );
    return { data: rows, total };
  }

  async deleteWish(id: number) {
    const result = await this.dataSource.query(
      'DELETE FROM wishes WHERE id = $1 RETURNING id',
      [id],
    );
    if (!result.length) throw new NotFoundException('Wish not found');
    return { deleted: true, id };
  }
}
