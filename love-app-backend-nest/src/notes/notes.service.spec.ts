import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { NotFoundException, ForbiddenException } from '@nestjs/common';
import { NotesService } from './notes.service.js';
import { Note } from './entities/note.entity.js';
import { CoupleService } from '../shared/couple.service.js';

const mockNote = {
  id: 1,
  user_id: 10,
  title: 'Test Note',
  content: 'Hello',
  is_private: false,
  deleted_at: new Date(),
  server_updated_at: new Date(),
  created_at: new Date(),
  tags: [],
  image_url: '',
  due_date: '',
} as Note;

const mockRepo = () => ({
  create: jest.fn().mockImplementation((dto) => ({ ...mockNote, ...dto })),
  save: jest.fn().mockImplementation((entity) => Promise.resolve({ ...mockNote, ...entity })),
  findOne: jest.fn(),
  createQueryBuilder: jest.fn(() => ({
    where: jest.fn().mockReturnThis(),
    andWhere: jest.fn().mockReturnThis(),
    orderBy: jest.fn().mockReturnThis(),
    skip: jest.fn().mockReturnThis(),
    take: jest.fn().mockReturnThis(),
    getManyAndCount: jest.fn().mockResolvedValue([[mockNote], 1]),
  })),
});

const mockCoupleService = () => ({
  getCoupleKey: jest.fn().mockResolvedValue('couple_10_20'),
  getPartnerId: jest.fn().mockResolvedValue(20),
  broadcastChange: jest.fn().mockResolvedValue(undefined),
});

describe('NotesService', () => {
  let service: NotesService;
  let repo: ReturnType<typeof mockRepo>;
  let couple: ReturnType<typeof mockCoupleService>;

  beforeEach(async () => {
    repo = mockRepo();
    couple = mockCoupleService();

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        NotesService,
        { provide: getRepositoryToken(Note), useValue: repo },
        { provide: CoupleService, useValue: couple },
      ],
    }).compile();

    service = module.get(NotesService);
  });

  describe('create', () => {
    it('should create a note and broadcast', async () => {
      const dto = { title: 'New', content: 'Body' };
      const result = await service.create(10, dto as any);

      expect(repo.create).toHaveBeenCalledWith({ ...dto, user_id: 10 });
      expect(repo.save).toHaveBeenCalled();
      expect(couple.getCoupleKey).toHaveBeenCalledWith(10);
      expect(couple.broadcastChange).toHaveBeenCalled();
      expect(result.title).toBe('New');
    });
  });

  describe('findAll', () => {
    it('should return paginated notes', async () => {
      const result = await service.findAll(10, { page: 1, limit: 20 });
      expect(result.items).toHaveLength(1);
      expect(result.total).toBe(1);
      expect(result.page).toBe(1);
    });
  });

  describe('findOne', () => {
    it('should return a note belonging to the user', async () => {
      repo.findOne.mockResolvedValue(mockNote);
      const result = await service.findOne(10, 1);
      expect(result.id).toBe(1);
    });

    it('should throw NotFoundException for missing note', async () => {
      repo.findOne.mockResolvedValue(null);
      await expect(service.findOne(10, 999)).rejects.toThrow(NotFoundException);
    });

    it('should throw ForbiddenException for private partner note', async () => {
      repo.findOne.mockResolvedValue({ ...mockNote, user_id: 20, is_private: true });
      await expect(service.findOne(10, 1)).rejects.toThrow(ForbiddenException);
    });

    it('should allow access to shared partner note', async () => {
      repo.findOne.mockResolvedValue({ ...mockNote, user_id: 20, is_private: false });
      const result = await service.findOne(10, 1);
      expect(result.user_id).toBe(20);
    });
  });

  describe('update', () => {
    it('should update the note and broadcast', async () => {
      repo.findOne.mockResolvedValue({ ...mockNote });
      const result = await service.update(10, 1, { title: 'Updated' } as any);
      expect(result.title).toBe('Updated');
      expect(couple.broadcastChange).toHaveBeenCalled();
    });

    it('should throw ForbiddenException if not owner', async () => {
      repo.findOne.mockResolvedValue({ ...mockNote, user_id: 99 });
      await expect(service.update(10, 1, {} as any)).rejects.toThrow(ForbiddenException);
    });
  });

  describe('remove', () => {
    it('should soft-delete and broadcast', async () => {
      repo.findOne.mockResolvedValue({ ...mockNote });
      const result = await service.remove(10, 1);
      expect(result).toEqual({ id: 1 });
      expect(repo.save).toHaveBeenCalled();
      expect(couple.broadcastChange).toHaveBeenCalled();
    });
  });
});
