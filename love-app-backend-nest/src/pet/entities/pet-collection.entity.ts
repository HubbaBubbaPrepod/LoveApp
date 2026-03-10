import { Entity, PrimaryGeneratedColumn, Column, Unique } from 'typeorm';

@Entity('pet_collections')
@Unique(['couple_key', 'pet_type_id'])
export class PetCollection {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column()
  couple_key: string;

  @Column()
  pet_type_id: number;

  @Column({ default: 'general' })
  collection_type: string;

  @Column({ type: 'timestamp', nullable: true })
  unlocked_at: Date;

  @Column({ type: 'timestamp', default: () => 'CURRENT_TIMESTAMP' })
  obtained_at: Date;
}
