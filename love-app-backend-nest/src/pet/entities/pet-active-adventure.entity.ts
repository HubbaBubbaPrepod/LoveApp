import { Entity, PrimaryGeneratedColumn, Column } from 'typeorm';

@Entity('pet_active_adventures')
export class PetActiveAdventure {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column()
  adventure_id: number;

  @Column({ type: 'timestamp' })
  started_at: Date;

  @Column({ type: 'timestamp' })
  ends_at: Date;

  @Column({ type: 'boolean', default: false })
  is_claimed: boolean;

  @Column({ type: 'int', nullable: true })
  reward_coins: number;

  @Column({ type: 'int', nullable: true })
  reward_xp: number;
}
