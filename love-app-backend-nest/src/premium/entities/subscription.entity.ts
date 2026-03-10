import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('subscriptions')
export class Subscription {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'int' })
  plan_id: number;

  @Column({ type: 'varchar' })
  purchase_token: string;

  @Column({ type: 'varchar', nullable: true })
  order_id: string;

  @Column({ type: 'varchar', default: 'active' })
  status: string;

  @Column({ type: 'timestamp' })
  expires_at: Date;

  @Column({ type: 'boolean', default: true })
  auto_renew: boolean;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
