import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('custom_calendars')
export class CustomCalendar {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar' })
  name: string;

  @Column({ type: 'varchar', nullable: true })
  color: string;

  @Column({ type: 'varchar', nullable: true })
  icon: string;

  @Column({ type: 'boolean', default: false })
  is_default: boolean;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
