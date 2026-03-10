import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('art_canvases')
export class ArtCanvas {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column()
  couple_key: string;

  @Column({ nullable: true })
  title: string;

  @Column({ type: 'text', nullable: true })
  thumbnail_url: string;

  @CreateDateColumn()
  created_at: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  updated_at: Date;
}
