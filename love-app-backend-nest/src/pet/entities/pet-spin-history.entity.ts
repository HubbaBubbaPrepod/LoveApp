import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

@Entity('pet_spin_history')
export class PetSpinHistory {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column()
  user_id: number;

  @Column()
  reward_type: string;

  @Column({ type: 'int' })
  reward_amount: number;

  @Column({ type: 'varchar', nullable: true })
  reward_detail: string;

  @CreateDateColumn({ name: 'spun_at' })
  spun_at: Date;
}
