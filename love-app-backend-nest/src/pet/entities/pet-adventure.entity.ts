import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

@Entity('pet_adventures')
export class PetAdventure {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  name: string;

  @Column({ nullable: true })
  description: string;

  @Column({ type: 'int' })
  duration_minutes: number;

  @Column({ type: 'int' })
  energy_cost: number;

  @Column({ type: 'int' })
  xp_reward: number;

  @Column({ type: 'int' })
  coin_reward_min: number;

  @Column({ type: 'int' })
  coin_reward_max: number;

  @Column({ type: 'int', default: 1 })
  min_level: number;

  @Column({ type: 'int', default: 0 })
  sort_order: number;

  @Column({ type: 'boolean', default: true })
  is_active: boolean;

  @CreateDateColumn()
  created_at: Date;
}
