import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn } from 'typeorm';

@Entity('pet_types')
export class PetType {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  name: string;

  @Column({ unique: true })
  code: string;

  @Column()
  display_name: string;

  @Column({ nullable: true })
  description: string;

  @Column({ nullable: true })
  base_image_url: string;

  @Column({ type: 'boolean', default: false })
  is_default: boolean;

  @Column({ type: 'int', default: 1 })
  unlock_level: number;

  @Column({ type: 'int', default: 0 })
  price_coins: number;

  @Column({ type: 'int', default: 0 })
  sort_order: number;

  @CreateDateColumn()
  created_at: Date;
}
