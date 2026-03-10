import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('user_achievements')
export class UserAchievement {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'int' })
  achievement_id: number;

  @Column({ type: 'varchar', nullable: true })
  couple_key: string;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  unlocked_at: Date;
}
