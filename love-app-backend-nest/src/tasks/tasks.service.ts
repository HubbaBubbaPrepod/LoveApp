import {
  Injectable,
  NotFoundException,
  ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, IsNull } from 'typeorm';
import { CoupleTask } from './entities/couple-task.entity';
import { CreateTaskDto } from './dto/create-task.dto';
import { CoupleService } from '../shared/couple.service';
import { FcmService } from '../shared/fcm.service';

const DAILY_SEED_TASKS = [
  { title: 'Отправь доброе утро', icon: '☀️', points: 5, category: 'daily' },
  { title: 'Напиши что-то приятное партнёру', icon: '💌', points: 10, category: 'daily' },
  { title: 'Поделись фото своего дня', icon: '📸', points: 10, category: 'daily' },
  { title: 'Расскажи о своём настроении', icon: '😊', points: 5, category: 'daily' },
  { title: 'Пожелай спокойной ночи', icon: '🌙', points: 5, category: 'daily' },
];

@Injectable()
export class TasksService {
  constructor(
    @InjectRepository(CoupleTask)
    private readonly repo: Repository<CoupleTask>,
    private readonly coupleService: CoupleService,
    private readonly fcmService: FcmService,
  ) {}

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private async seedDailyTasks(coupleKey: string, userId: number) {
    const today = this.today();
    const existing = await this.repo.findOne({
      where: { couple_key: coupleKey, is_system: true, due_date: today, deleted_at: IsNull() },
    });
    if (existing) return;

    const tasks = DAILY_SEED_TASKS.map((t) =>
      this.repo.create({
        ...t,
        couple_key: coupleKey,
        user_id: userId,
        due_date: today,
        is_system: true,
      }),
    );
    await this.repo.save(tasks);
  }

  async findAll(userId: number, query: { date?: string }) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const date = query.date || this.today();

    await this.seedDailyTasks(coupleKey, userId);

    const tasks = await this.repo.find({
      where: { couple_key: coupleKey, due_date: date, deleted_at: IsNull() },
      order: { is_system: 'DESC', created_at: 'ASC' },
    });

    const totalPoints = tasks
      .filter((t) => t.is_completed)
      .reduce((sum, t) => sum + (t.points || 0), 0);

    return { tasks, total_points: totalPoints };
  }

  async create(userId: number, dto: CreateTaskDto) {
    const coupleKey = await this.coupleService.getCoupleKey(userId);
    const entity = this.repo.create({
      ...dto,
      user_id: userId,
      couple_key: coupleKey,
      due_date: dto.due_date || this.today(),
    });
    const saved = await this.repo.save(entity);
    await this.coupleService.broadcastChange(coupleKey, userId, 'couple_task', 'create', saved);
    return saved;
  }

  async complete(userId: number, id: number) {
    const record = await this.repo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!record) throw new NotFoundException('Task not found');

    record.is_completed = true;
    record.completed_by = userId;
    record.completed_at = new Date();
    record.server_updated_at = new Date();
    const saved = await this.repo.save(record);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'couple_task', 'update', saved);

    await this.fcmService.sendPushToPartner(userId, {
      type: 'task_completed',
      title: '✅ Задание выполнено!',
      body: record.title,
      destination: 'tasks',
    });

    return saved;
  }

  async remove(userId: number, id: number) {
    const record = await this.repo.findOne({
      where: { id, deleted_at: IsNull() },
    });
    if (!record) throw new NotFoundException('Task not found');
    if (record.is_system) throw new ForbiddenException('Cannot delete system tasks');
    if (Number(record.user_id) !== userId) throw new ForbiddenException();

    record.deleted_at = new Date();
    record.server_updated_at = new Date();
    await this.repo.save(record);

    const coupleKey = await this.coupleService.getCoupleKey(userId);
    await this.coupleService.broadcastChange(coupleKey, userId, 'couple_task', 'delete', { id });
    return { deleted: true };
  }
}
