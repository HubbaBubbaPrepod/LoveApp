import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  Unique,
} from 'typeorm';

@Entity('sleep_entries')
@Unique(['user_id', 'date'])
export class SleepEntry {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'date' })
  date: string;

  @Column({ type: 'varchar', nullable: true })
  bedtime: string;

  @Column({ type: 'varchar', nullable: true })
  wake_time: string;

  @Column({ type: 'int', nullable: true })
  duration_minutes: number;

  @Column({ type: 'varchar', nullable: true })
  quality: string;

  @Column({ type: 'text', nullable: true })
  note: string;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
