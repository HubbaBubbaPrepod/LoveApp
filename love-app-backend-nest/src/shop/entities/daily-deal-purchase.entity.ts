import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
} from 'typeorm';

@Entity('daily_deal_purchases')
export class DailyDealPurchase {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'int' })
  deal_id: number;

  @Column()
  couple_key: string;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  purchased_at: Date;
}
