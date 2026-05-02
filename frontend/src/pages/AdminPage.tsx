import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { suggestionsApi } from "@/api/endpoints";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { formatRelativeTime } from "@/lib/utils";
import { useState } from "react";
import { Check, X } from "lucide-react";

export function AdminPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["admin", "pending"],
    queryFn: () => suggestionsApi.pending(0, 50),
  });

  return (
    <div className="container max-w-3xl py-8">
      <h1 className="text-xl font-bold mb-6">Admin — Pending Suggestions</h1>
      {isLoading && <p className="text-muted-foreground">Loading...</p>}
      {data?.content.length === 0 && <p className="text-muted-foreground">No pending suggestions.</p>}
      <div className="space-y-4">
        {data?.content.map((s) => (
          <ReviewCard key={s.id} suggestion={s} onReviewed={() => qc.invalidateQueries({ queryKey: ["admin"] })} />
        ))}
      </div>
    </div>
  );
}

function ReviewCard({ suggestion: s, onReviewed }: { suggestion: { id: string; type: string; username: string; payload: Record<string, unknown>; rationale?: string; createdAt: string }; onReviewed: () => void }) {
  const [notes, setNotes] = useState("");

  const approve = useMutation({
    mutationFn: () => suggestionsApi.review(s.id, "APPROVED", notes || undefined),
    onSuccess: onReviewed,
  });

  const reject = useMutation({
    mutationFn: () => suggestionsApi.review(s.id, "REJECTED", notes || undefined),
    onSuccess: onReviewed,
  });

  return (
    <Card>
      <CardContent className="p-4 space-y-3">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2">
              <Badge variant="outline" className="text-[10px]">{s.type.replace("_", " ")}</Badge>
              <span className="text-xs text-muted-foreground">by {s.username} · {formatRelativeTime(s.createdAt)}</span>
            </div>
            {s.rationale && <p className="text-sm mt-1">{s.rationale}</p>}
            <pre className="text-xs bg-muted p-2 rounded mt-2 max-h-32 overflow-auto">{JSON.stringify(s.payload, null, 2)}</pre>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Input placeholder="Review notes (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} className="flex-1" />
          <Button size="sm" onClick={() => approve.mutate()} disabled={approve.isPending}>
            <Check className="h-4 w-4 mr-1" /> Approve
          </Button>
          <Button size="sm" variant="destructive" onClick={() => reject.mutate()} disabled={reject.isPending}>
            <X className="h-4 w-4 mr-1" /> Reject
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
