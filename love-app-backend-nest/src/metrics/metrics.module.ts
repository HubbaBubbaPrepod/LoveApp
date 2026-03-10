import { Module } from '@nestjs/common';
import {
  PrometheusModule,
  makeHistogramProvider,
  makeGaugeProvider,
} from '@willsoto/nestjs-prometheus';
import { MetricsService } from './metrics.service';
import { HttpMetricsInterceptor } from './interceptors/http-metrics.interceptor';

@Module({
  imports: [
    PrometheusModule.register({
      path: '/metrics',
      defaultMetrics: { enabled: true },
    }),
  ],
  providers: [
    makeHistogramProvider({
      name: 'http_request_duration_seconds',
      help: 'Duration of HTTP requests in seconds',
      labelNames: ['method', 'route', 'status_code'],
      buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10],
    }),
    makeGaugeProvider({
      name: 'active_socket_connections',
      help: 'Number of active WebSocket connections',
    }),
    makeHistogramProvider({
      name: 'db_query_duration_seconds',
      help: 'Duration of database queries in seconds',
      labelNames: ['operation'],
      buckets: [0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1, 5],
    }),
    MetricsService,
    HttpMetricsInterceptor,
  ],
  exports: [MetricsService, HttpMetricsInterceptor],
})
export class MetricsModule {}
