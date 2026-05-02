import { useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { questionsApi } from "@/api/endpoints";
import { useAuthStore } from "@/store/auth";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ThumbsUp, Eye, MessageCircle, Calendar } from "lucide-react";
import { formatRelativeTime } from "@/lib/utils";

const diffColors: Record<string, string> = {
  EASY: "bg-green-100 text-green-800",
  MEDIUM: "bg-amber-100 text-amber-800",
  HARD: "bg-red-100 text-red-800",
};

export function QuestionPage() {
  const { id } = useParams<{ id: string }>();
  const user = useAuthStore((s) => s.user);
  const qc = useQueryClient();

  const { data: q, isLoading } = useQuery({
    queryKey: ["question", id],
    queryFn: () => questionsApi.byId(id!),
    enabled: !!id,
  });

  const likeMut = useMutation({
    mutationFn: () => questionsApi.like(id!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["question", id] }),
  });

  const unlikeMut = useMutation({
    mutationFn: () => questionsApi.unlike(id!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["question", id] }),
  });

  if (isLoading) {
    return (
      <div className="container max-w-3xl py-8 space-y-4">
        <Skeleton className="h-10 w-3/4" />
        <Skeleton className="h-6 w-1/2" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!q) {
    return <div className="container py-12 text-center text-muted-foreground">Question not found</div>;
  }

  return (
    <div className="container max-w-3xl py-8">
      <Card>
        <CardContent className="p-6 space-y-4">
          {/* Header */}
          <div>
            <h1 className="text-xl font-bold leading-tight">{q.title}</h1>
            <div className="flex flex-wrap items-center gap-2 mt-2">
              <Badge variant="outline">{q.topicName}</Badge>
              <Badge variant="outline" className={diffColors[q.difficulty] || ""}>{q.difficulty}</Badge>
              {q.tags?.map((tag) => (
                <Badge key={tag} variant="secondary" className="text-xs">{tag}</Badge>
              ))}
            </div>
          </div>

          {/* Meta row */}
          <div className="flex items-center gap-4 text-sm text-muted-foreground border-y py-3">
            <span className="flex items-center gap-1"><Eye className="h-4 w-4" /> {q.viewCount} views</span>
            <span className="flex items-center gap-1"><ThumbsUp className="h-4 w-4" /> {q.likeCount} likes</span>
            <span className="flex items-center gap-1"><MessageCircle className="h-4 w-4" /> {q.answerCount} answers</span>
            <span className="flex items-center gap-1"><Calendar className="h-4 w-4" /> {formatRelativeTime(q.createdAt)}</span>
            <span>by <strong>{q.authorUsername}</strong></span>
          </div>

          {/* Content */}
          <div className="prose prose-sm max-w-none whitespace-pre-wrap">{q.content}</div>

          {/* Actions */}
          {user && (
            <div className="flex gap-2 pt-2 border-t">
              <Button variant="outline" size="sm" onClick={() => likeMut.mutate()} disabled={likeMut.isPending}>
                <ThumbsUp className="h-4 w-4 mr-1" /> Like
              </Button>
              <Button variant="ghost" size="sm" onClick={() => unlikeMut.mutate()} disabled={unlikeMut.isPending}>
                Unlike
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Answers placeholder */}
      <div className="mt-6 p-6 border rounded-lg text-center text-muted-foreground">
        Answers section coming soon — wire the AnswerService backend to populate this.
      </div>
    </div>
  );
}
