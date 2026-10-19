package com.example.orderimport.config;

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch のメタデータを DB に永続化する設定。
 *
 * <p><strong>この設定を外すと再開できなくなる。</strong>Spring Batch 6 の既定の
 * ジョブリポジトリは {@code ResourcelessJobRepository}、つまり何も永続化しない
 * 実装である。Spring Boot の自動設定もこれをそのまま採用するため、放っておくと
 * ステップの実行状況も読み取り位置も残らず、「失敗した所から再開する」という
 * 仕様が静かに成立しなくなる（ジョブは動くので、再開を試すまで気づけない）。
 *
 * <p>{@link JdbcDefaultBatchConfiguration} を継承することで、
 * <ul>
 *   <li>JDBC 版のジョブリポジトリが使われる（{@code dataSource} Bean を利用）</li>
 *   <li>Spring Boot 側の自動設定は
 *       {@code @ConditionalOnMissingBean(DefaultBatchConfiguration.class)} により
 *       自動的に引き下がる</li>
 *   <li>ステップスコープ・ジョブスコープは基底クラス経由で従来どおり有効</li>
 * </ul>
 *
 * <p>メタデータ表の DDL は {@code src/main/resources/db/mysql/01-spring-batch-metadata.sql}
 * にある。バッチは DDL を自動実行しないので、運用手順で適用すること。
 */
@Configuration
public class BatchInfrastructureConfig extends JdbcDefaultBatchConfiguration {
}
