import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

@Entity('pet_wishes')
export class PetWish {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column()
  user_id: number;

  @Column()
  content: string;

  @Column({ type: 'boolean', default: false })
  is_fulfilled: boolean;

  @Column({ type: 'timestamp', nullable: true })
  fulfilled_at: Date;

  @Column({ type: 'int', default: 10 })
  xp_reward: number;

  @CreateDateColumn()
  created_at: Date;
}
