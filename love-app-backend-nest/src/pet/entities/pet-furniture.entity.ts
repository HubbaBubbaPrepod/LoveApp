import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

@Entity('pet_furniture')
export class PetFurniture {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  name: string;

  @Column({ nullable: true })
  description: string;

  @Column({ nullable: true })
  category: string;

  @Column({ nullable: true })
  image_url: string;

  @Column({ type: 'int' })
  price_coins: number;

  @Column({ nullable: true })
  effect_type: string;

  @Column({ type: 'int', nullable: true })
  effect_amount: number;

  @Column({ type: 'int', default: 0 })
  sort_order: number;

  @Column({ type: 'boolean', default: true })
  is_available: boolean;

  @CreateDateColumn()
  created_at: Date;
}
