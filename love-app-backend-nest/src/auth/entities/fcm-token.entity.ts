import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity('fcm_tokens')
export class FcmToken {
  @PrimaryGeneratedColumn('increment')
  id: number;

  @Column({ type: 'bigint' })
  user_id: number;

  @Column()
  fcm_token: string;

  @Column({ nullable: true })
  device_id: string;

  @UpdateDateColumn()
  updated_at: Date;
}
