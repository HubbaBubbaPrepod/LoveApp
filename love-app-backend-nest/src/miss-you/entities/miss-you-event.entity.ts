import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('miss_you_events')
export class MissYouEvent {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint' })
  sender_id: number;

  @Column({ type: 'bigint', nullable: true })
  receiver_id: number | null;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'varchar', default: '❤️' })
  emoji: string;

  @Column({ type: 'text', nullable: true })
  message: string;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
