import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('chat_messages')
export class ChatMessage {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  sender_id: number;

  @Column({ type: 'bigint' })
  receiver_id: number;

  @Column()
  couple_key: string;

  @Column({ default: 'text' })
  message_type: string;

  @Column({ type: 'text', nullable: true })
  content: string | null;

  @Column({ type: 'text', nullable: true })
  image_url: string | null;

  @Column({ type: 'text', nullable: true })
  voice_url: string | null;

  @Column({ type: 'int', nullable: true })
  voice_duration: number | null;

  @Column({ type: 'text', nullable: true })
  video_url: string | null;

  @Column({ type: 'int', nullable: true })
  video_duration: number | null;

  @Column({ type: 'text', nullable: true })
  video_thumbnail_url: string | null;

  @Column({ type: 'numeric', nullable: true })
  latitude: number | null;

  @Column({ type: 'numeric', nullable: true })
  longitude: number | null;

  @Column({ nullable: true })
  location_name: string;

  @Column({ type: 'int', nullable: true })
  sticker_id: number;

  @Column({ type: 'text', nullable: true })
  drawing_data: string;

  @Column({ nullable: true })
  emoji: string;

  @Column({ type: 'bigint', nullable: true })
  reply_to_id: number;

  @Column({ type: 'boolean', default: false })
  is_read: boolean;

  @Column({ type: 'timestamp', nullable: true })
  read_at: Date;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
