import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  Unique,
} from 'typeorm';

@Entity('user_mission_progress')
@Unique(['user_id', 'mission_id', 'mission_date'])
export class UserMissionProgress {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'int' })
  mission_id: number;

  @Column({ type: 'int', default: 0 })
  current_count: number;

  @Column({ type: 'boolean', default: false })
  is_completed: boolean;

  @Column({ type: 'timestamp', nullable: true })
  completed_at: Date;

  @Column({ type: 'boolean', default: false })
  is_claimed: boolean;

  @Column({ type: 'timestamp', nullable: true })
  claimed_at: Date;

  @Column({ type: 'date' })
  mission_date: string;
}
