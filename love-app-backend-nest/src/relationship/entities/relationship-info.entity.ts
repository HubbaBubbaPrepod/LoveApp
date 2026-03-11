import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('relationship_info')
export class RelationshipInfo {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint', nullable: true })
  user_id: number;

  @Column({ type: 'bigint', nullable: true, name: 'partner_user_id' })
  partner_id: number;

  @Column({ nullable: true })
  couple_key: string;

  @Column({ nullable: true, name: 'relationship_start_date' })
  start_date: string;

  @Column({ nullable: true })
  anniversary_date: string;

  @Column({ type: 'date', nullable: true })
  first_kiss_date: string;

  @Column({ type: 'date', nullable: true })
  my_birthday: string;

  @Column({ type: 'date', nullable: true })
  partner_birthday: string;

  @Column({ nullable: true })
  nickname1: string;

  @Column({ nullable: true })
  nickname2: string;

  @Column({ type: 'text', nullable: true })
  notes: string;

  @Column({ nullable: true })
  pairing_code: string;

  @Column({ type: 'timestamp', nullable: true })
  pairing_expires: Date;

  @Column({ type: 'timestamp', default: () => 'NOW()' })
  server_updated_at: Date;

  @CreateDateColumn()
  created_at: Date;

  @UpdateDateColumn()
  updated_at: Date;
}
