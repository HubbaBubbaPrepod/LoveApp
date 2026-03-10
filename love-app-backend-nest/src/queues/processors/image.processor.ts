import { Processor, WorkerHost } from '@nestjs/bullmq';
import { Inject } from '@nestjs/common';
import { Job } from 'bullmq';
import { WINSTON_MODULE_PROVIDER } from 'nest-winston';
import { Logger } from 'winston';
import * as fs from 'fs';
import * as path from 'path';

@Processor('imageProcessing')
export class ImageProcessor extends WorkerHost {
  constructor(
    @Inject(WINSTON_MODULE_PROVIDER) private readonly logger: Logger,
  ) {
    super();
  }

  async process(job: Job): Promise<any> {
    const { inputPath, outputPath, width = 1920, quality = 80, format = 'webp' } = job.data;

    try {
      if (!fs.existsSync(inputPath)) {
        throw new Error(`Input file not found: ${inputPath}`);
      }

      const dir = path.dirname(outputPath);
      fs.mkdirSync(dir, { recursive: true });

      // Dynamic import for sharp (optional dependency)
      const sharp = (await import('sharp')).default;
      const metadata = await sharp(inputPath).metadata();

      let pipeline = sharp(inputPath);

      if (metadata.width && metadata.width > width) {
        pipeline = pipeline.resize(width);
      }

      if (format === 'webp') {
        pipeline = pipeline.webp({ quality });
      } else {
        pipeline = pipeline.jpeg({ quality });
      }

      await pipeline.toFile(outputPath);

      this.logger.info(`Image processed: ${inputPath} → ${outputPath}`);
      return { outputPath };
    } catch (err: any) {
      this.logger.error(`Image processing failed: ${err.message}`);
      throw err;
    }
  }
}
