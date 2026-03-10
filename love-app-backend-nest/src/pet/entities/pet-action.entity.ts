import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

@Entity('pet_actions')
export class PetAction {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column()
  user_id: number;

  @Column()
  action_type: string;

  @Column({ type: 'int', default: 0 })
  xp_gained: number;

  @CreateDateColumn()
  created_at: Date;
}
