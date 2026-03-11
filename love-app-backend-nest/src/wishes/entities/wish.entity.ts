import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('wishes')
export class Wish {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint', nullable: true })
  user_id: number;

  @Column({ nullable: true })
  title: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'int', default: 3 })
  priority: number;

  @Column({ nullable: true })
  category: string;

  @Column({ nullable: true })
  emoji: string;

  @Column({ type: 'text', nullable: true })
  image_urls: string;

  @Column({ type: 'text', nullable: true })
  image_url: string;

  @Column({ type: 'boolean', default: false })
  is_private: boolean;

  @Column({ type: 'boolean', default: false })
  is_completed: boolean;

  @Column({ type: 'date', nullable: true })
  due_date: string;

  @Column({ type: 'timestamp', nullable: true })
  completed_at: Date;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @Column({ type: 'timestamp', nullable: true })
  updated_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
