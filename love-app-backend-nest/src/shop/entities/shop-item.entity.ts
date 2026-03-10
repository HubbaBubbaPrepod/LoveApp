import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('shop_items')
export class ShopItem {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  name: string;

  @Column()
  category: string;

  @Column({ type: 'boolean', default: true })
  is_available: boolean;

  @Column({ type: 'int' })
  price_coins: number;

  @Column({ type: 'varchar' })
  effect_type: string;

  @Column({ type: 'int' })
  effect_amount: number;

  @Column({ type: 'int', default: 0 })
  sort_order: number;

  @CreateDateColumn()
  created_at: Date;
}
