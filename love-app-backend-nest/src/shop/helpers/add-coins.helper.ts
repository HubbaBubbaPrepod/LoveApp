import { DataSource } from 'typeorm';

/**
 * Add (or deduct) coins transactionally.
 * For positive amounts, applies premium_coins_multiplier from users table.
 * Updates pets.coins and creates a coin_transaction record.
 */
export async function addCoins(
  dataSource: DataSource,
  coupleKey: string,
  userId: number,
  amount: number,
  txType: string,
  description: string,
  refType?: string,
  refId?: number,
): Promise<{ finalAmount: number; balanceBefore: number; balanceAfter: number }> {
  return dataSource.transaction(async (manager) => {
    // Get current balance
    const pet = await manager.query(
      `SELECT coins FROM pets WHERE couple_key = $1 FOR UPDATE`,
      [coupleKey],
    );
    const balanceBefore = pet?.[0]?.coins ?? 0;

    let finalAmount = amount;
    if (amount > 0) {
      // Apply premium multiplier for positive amounts
      const user = await manager.query(
        `SELECT premium_coins_multiplier FROM users WHERE id = $1`,
        [userId],
      );
      const multiplier = user?.[0]?.premium_coins_multiplier ?? 1;
      finalAmount = Math.floor(amount * multiplier);
    }

    const balanceAfter = balanceBefore + finalAmount;

    // Update pets balance
    await manager.query(
      `UPDATE pets SET coins = $1 WHERE couple_key = $2`,
      [balanceAfter, coupleKey],
    );

    // Create transaction record
    await manager.query(
      `INSERT INTO coin_transactions (couple_key, user_id, tx_type, amount, balance_before, balance_after, ref_type, ref_id, description)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [coupleKey, userId, txType, finalAmount, balanceBefore, balanceAfter, refType ?? null, refId ?? null, description],
    );

    return { finalAmount, balanceBefore, balanceAfter };
  });
}
