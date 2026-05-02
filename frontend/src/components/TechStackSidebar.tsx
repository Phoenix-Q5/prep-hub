import { useQuery } from "@tanstack/react-query";
import { topicsApi } from "@/api/endpoints";
import { Badge } from "@/components/ui/badge";

export function TechStackSidebar() {
  const { data: topics } = useQuery({
    queryKey: ["topics", "featured"],
    queryFn: () => topicsApi.list(true),
  });

  return (
    <aside className="w-56 shrink-0">
      <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-3">Tech Stacks</h2>
      <div className="space-y-2">
        {topics?.map((t) => (
          <div key={t.id} className="flex items-center gap-2 rounded-md border px-3 py-2">
            {t.colorHex && (
              <span className="w-3 h-3 rounded-sm shrink-0" style={{ backgroundColor: t.colorHex }} />
            )}
            <span className="text-sm font-medium flex-1">{t.name}</span>
            <Badge variant="secondary" className="text-[10px]">{t.questionCount}</Badge>
          </div>
        ))}
      </div>
    </aside>
  );
}
