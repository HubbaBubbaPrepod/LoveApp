import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('custom_calendar_events')
export class CustomCalendarEvent {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  calendar_id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar', nullable: true })
  couple_key: string;

  @Column({ type: 'varchar' })
  title: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'date' })
  event_date: string;

  @Column({ type: 'varchar', nullable: true })
  start_time: string;

  @Column({ type: 'varchar', nullable: true })
  end_time: string;

  @Column({ type: 'varchar', nullable: true })
  location: string;

  @Column({ type: 'varchar', nullable: true })
  color: string;

  @Column({ type: 'varchar', nullable: true })
  icon: string;

  @Column({ type: 'boolean', default: false })
  is_recurring: boolean;

  @Column({ type: 'varchar', nullable: true })
  recurrence_rule: string;

  @Column({ type: 'int', nullable: true })
  reminder_minutes: number;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
