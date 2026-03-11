import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('menstrual_cycles')
export class MenstrualCycle {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint', nullable: true })
  user_id: number;

  @Column({ type: 'date', nullable: true, name: 'cycle_start_date' })
  start_date: string;

  @Column({ type: 'date', nullable: true })
  end_date: string;

  @Column({ type: 'smallint', nullable: true, name: 'cycle_duration' })
  cycle_length: number;

  @Column({ type: 'smallint', nullable: true, name: 'period_duration' })
  period_length: number;

  @Column({ type: 'jsonb', default: '{}' })
  symptoms: Record<string, any>;

  @Column({ type: 'jsonb', default: '{}' })
  mood: Record<string, any>;

  @Column({ type: 'text', nullable: true })
  notes: string;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
