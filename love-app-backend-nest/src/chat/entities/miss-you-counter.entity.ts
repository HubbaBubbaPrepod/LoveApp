import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  Unique,
} from 'typeorm';

@Entity('miss_you_counter')
@Unique(['couple_key', 'user_id', 'date'])
export class MissYouCounter {
  @PrimaryGeneratedColumn('increment', { type: 'bigint' })
  id: number;

  @Column()
  couple_key: string;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'date' })
  date: string;

  @Column({ type: 'int', default: 0 })
  count: number;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  updated_at: Date;
}
