import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity('relationship_info')
export class RelationshipInfo {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column({ type: 'bigint', nullable: true })
  partner_id: number;

  @Column({ nullable: true })
  couple_key: string;

  @Column({ nullable: true })
  start_date: string;

  @Column({ nullable: true })
  anniversary_date: string;

  @Column({ nullable: true })
  pairing_code: string;

  @Column({ type: 'timestamp', nullable: true })
  pairing_expires: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
