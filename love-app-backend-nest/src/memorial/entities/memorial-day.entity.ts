import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('memorial_days')
export class MemorialDay {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'varchar' })
  title: string;

  @Column({ type: 'date' })
  date: string;

  @Column({ type: 'varchar', default: 'special' })
  type: string;

  @Column({ type: 'varchar', default: '❤️' })
  icon: string;

  @Column({ type: 'varchar', default: '#FF6B9D' })
  color_hex: string;

  @Column({ type: 'boolean', default: true })
  repeat_yearly: boolean;

  @Column({ type: 'int', default: 1 })
  reminder_days: number;

  @Column({ type: 'text', nullable: true })
  note: string;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
