import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity('coin_transactions')
export class CoinTransaction {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'varchar' })
  tx_type: string;

  @Column({ type: 'int' })
  amount: number;

  @Column({ type: 'int' })
  balance_before: number;

  @Column({ type: 'int' })
  balance_after: number;

  @Column({ type: 'varchar', nullable: true })
  ref_type: string;

  @Column({ type: 'int', nullable: true })
  ref_id: number;

  @Column({ type: 'text', nullable: true })
  description: string;

  @CreateDateColumn()
  created_at: Date;
}
