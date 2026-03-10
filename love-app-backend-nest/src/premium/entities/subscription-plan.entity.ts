import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
} from 'typeorm';

@Entity('subscription_plans')
export class SubscriptionPlan {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  title: string;

  @Column()
  google_play_id: string;

  @Column({ type: 'int' })
  duration_months: number;

  @Column({ type: 'varchar' })
  plan_type: string;

  @Column({ type: 'boolean', default: true })
  active: boolean;

  @Column({ type: 'int', default: 0 })
  sort_order: number;
}
