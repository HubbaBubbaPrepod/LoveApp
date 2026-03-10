import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('daily_deals')
export class DailyDeal {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  item_name: string;

  @Column({ type: 'boolean', default: true })
  is_active: boolean;

  @Column({ type: 'timestamp' })
  valid_until: Date;

  @Column({ type: 'int' })
  deal_price: number;

  @Column({ type: 'int' })
  total_available: number;

  @Column({ type: 'int', default: 0 })
  redeemed_count: number;

  @CreateDateColumn()
  created_at: Date;
}
