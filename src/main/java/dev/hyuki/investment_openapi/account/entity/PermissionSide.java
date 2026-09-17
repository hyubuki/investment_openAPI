package dev.hyuki.investment_openapi.account.entity;

public enum PermissionSide {
  BUY,
  SELL,
  BOTH;

  public static PermissionSide forOrder(OrderSide orderSide) {
    return switch (orderSide) {
      case BUY -> BUY;
      case SELL -> SELL;
    };
  }
}
