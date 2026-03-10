import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('story_entries')
export class StoryEntry {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column()
  couple_key: string;

  @Column({ type: 'bigint' })
  author_id: number;

  @Column({ type: 'varchar', length: 200, nullable: true })
  title: string;

  @Column({ type: 'text', nullable: true })
  content: string;

  @Column({ type: 'varchar', default: 'text' })
  entry_type: string;

  @Column({ type: 'date', nullable: true })
  entry_date: string;

  @Column({ type: 'text', nullable: true })
  media_url: string;

  @Column({ type: 'varchar', default: '❤️' })
  emoji: string;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;
}
