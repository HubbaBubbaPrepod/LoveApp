import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('moments')
export class Moment {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar' })
  couple_key: string;

  @Column({ type: 'text', nullable: true })
  content: string;

  @Column({ type: 'text', nullable: true })
  image_url: string;

  @Column({ type: 'varchar', nullable: true })
  mood: string;

  @Column({ type: 'varchar', nullable: true })
  location_name: string;

  @Column({ type: 'numeric', nullable: true })
  latitude: number;

  @Column({ type: 'numeric', nullable: true })
  longitude: number;

  @CreateDateColumn()
  created_at: Date;

  @Column({ type: 'timestamp', nullable: true })
  deleted_at: Date;
}
