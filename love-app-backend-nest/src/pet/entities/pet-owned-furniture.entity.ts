import { Entity, PrimaryGeneratedColumn, Column } from 'typeorm';

@Entity('pet_owned_furniture')
export class PetOwnedFurniture {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column()
  furniture_id: number;

  @Column({ type: 'boolean', default: false })
  is_placed: boolean;

  @Column({ type: 'int', nullable: true })
  position_x: number;

  @Column({ type: 'int', nullable: true })
  position_y: number;

  @Column({ type: 'timestamp', default: () => 'CURRENT_TIMESTAMP' })
  purchased_at: Date;
}
