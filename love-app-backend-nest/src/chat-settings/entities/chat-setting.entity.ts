import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity('chat_settings')
export class ChatSetting {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint', unique: true })
  user_id: number;

  @Column({ type: 'text', nullable: true })
  wallpaper_url: string;

  @Column({ type: 'varchar', default: 'pink' })
  bubble_color: string;

  @Column({ type: 'varchar', default: 'rounded' })
  bubble_shape: string;

  @UpdateDateColumn()
  updated_at: Date;
}
