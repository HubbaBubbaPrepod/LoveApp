import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('love_letters')
export class LoveLetter {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint' })
  sender_id: number;

  @Column({ type: 'bigint', nullable: true })
  receiver_id: number | null;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'varchar' })
  title: string;

  @Column({ type: 'text' })
  content: string;

  @Column({ type: 'varchar', nullable: true })
  mood: string;

  @Column({ type: 'text', nullable: true })
  image_url: string;

  @Column({ type: 'date', nullable: true })
  open_date: string;

  @Column({ type: 'boolean', default: false })
  is_opened: boolean;

  @Column({ type: 'timestamp', nullable: true })
  opened_at: Date;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
