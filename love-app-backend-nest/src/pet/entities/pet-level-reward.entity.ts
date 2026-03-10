import { Entity, PrimaryGeneratedColumn, Column } from 'typeorm';

@Entity('pet_level_rewards')
export class PetLevelReward {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'int', unique: true })
  level: number;

  @Column()
  reward_type: string;

  @Column({ type: 'int' })
  reward_amount: number;

  @Column({ nullable: true })
  description: string;
}
