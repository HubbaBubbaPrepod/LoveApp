import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
} from 'typeorm';

@Entity('daily_missions')
export class DailyMission {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  name: string;

  @Column({ type: 'varchar', unique: true })
  code: string;

  @Column({ type: 'varchar' })
  mission_type: string;

  @Column({ type: 'int' })
  target_count: number;

  @Column({ type: 'int' })
  reward_coins: number;

  @Column({ type: 'int' })
  reward_xp: number;

  @Column({ type: 'boolean', default: true })
  is_active: boolean;

  @Column({ type: 'int', default: 0 })
  sort_order: number;
}
