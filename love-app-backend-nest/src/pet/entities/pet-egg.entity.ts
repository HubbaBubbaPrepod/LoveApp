import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

@Entity('pet_eggs')
export class PetEgg {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column()
  pet_type_id: number;

  @Column()
  name: string;

  @Column({ default: 'common' })
  rarity: string;

  @Column({ type: 'boolean', default: false })
  is_hatched: boolean;

  @Column({ type: 'timestamp', nullable: true })
  hatched_at: Date;

  @CreateDateColumn()
  created_at: Date;
}
