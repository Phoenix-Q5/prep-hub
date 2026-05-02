import { Link, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { topicsApi } from "@/api/endpoints";
import { Skeleton } from "@/components/ui/skeleton";

export function TopicsSidebar() {
  const { data: topics, isLoading } = useQuery({
    queryKey: ["topics"],
    queryFn: () => topicsApi.list(),
  });
  const [params] = useSearchParams();
  const activeTopic = params.get("topic");

  return (
    <aside className="w-64 shrink-0">
      <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Topics</h2>
      <nav className="space-y-1">
        <Link
          to="/"
          className={`block px-3 py-2 rounded-md text-sm transition-colors ${
            !activeTopic ? "bg-accent font-medium" : "hover:bg-accent/50"
          }`}
        >
          All Questions
        </Link>
        {isLoading && Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="h-9 w-full" />
        ))}
        {topics?.map((t) => (
          <Link
            key={t.id}
            to={`/?topic=${t.slug}`}
            className={`flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors ${
              activeTopic === t.slug ? "bg-accent font-medium" : "hover:bg-accent/50"
            }`}
          >
            {t.colorHex && (
              <span className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: t.colorHex }} />
            )}
            <span className="flex-1 truncate">{t.name}</span>
            <span className="text-xs text-muted-foreground">{t.questionCount}</span>
          </Link>
        ))}
      </nav>
    </aside>
  );
}
