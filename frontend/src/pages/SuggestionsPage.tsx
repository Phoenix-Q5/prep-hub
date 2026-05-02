import { useQuery } from "@tanstack/react-query";
import { suggestionsApi } from "@/api/endpoints";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { formatRelativeTime } from "@/lib/utils";

const statusColors: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  APPROVED: "bg-green-100 text-green-800",
  REJECTED: "bg-red-100 text-red-800",
};

export function SuggestionsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["suggestions", "mine"],
    queryFn: () => suggestionsApi.mine(0, 50),
  });

  return (
    <div className="container max-w-3xl py-8">
      <h1 className="text-xl font-bold mb-6">My Suggestions</h1>
      <div className="space-y-3">
        {isLoading && <p className="text-muted-foreground">Loading...</p>}
        {data?.content.length === 0 && <p className="text-muted-foreground">No suggestions yet.</p>}
        {data?.content.map((s) => (
          <Card key={s.id}>
            <CardContent className="p-4">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <Badge variant="outline" className="text-[10px]">{s.type.replace("_", " ")}</Badge>
                    <Badge className={`text-[10px] ${statusColors[s.status] || ""}`}>{s.status}</Badge>
                  </div>
                  <p className="text-sm">{s.rationale || JSON.stringify(s.payload).slice(0, 120)}</p>
                  <p className="text-xs text-muted-foreground mt-1">{formatRelativeTime(s.createdAt)}</p>
                  {s.reviewNotes && (
                    <p className="text-xs text-muted-foreground mt-1 italic">Review: {s.reviewNotes}</p>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
