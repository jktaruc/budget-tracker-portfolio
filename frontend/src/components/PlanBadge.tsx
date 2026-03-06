import { Link } from "react-router-dom";
import { useSubscription } from "../context/SubscriptionContext";
import "../styles/PlanBadge.css";

export default function PlanBadge() {
  const { plan, cancelling, isLoading } = useSubscription();
  if (isLoading) return null;

  return (
    <Link to="/billing" className={`plan-badge plan-badge--${plan.toLowerCase()}${cancelling ? " plan-badge--cancelling" : ""}`}>
      {plan === "PRO" ? (cancelling ? "⭐ Pro (cancelling)" : "⭐ Pro") : "Free"}
    </Link>
  );
}
