import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { usersApi } from "@/api/endpoints";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Trophy, ThumbsUp, Lightbulb, FileText } from "lucide-react";
import { formatRelativeTime } from "@/lib/utils";

export function ProfilePage() {
  const { username } = useParams<{ username: string }>();

  const { data: profile, isLoading } = useQuery({
    queryKey: ["profile", username],
    queryFn: () => usersApi.byUsername(username!),
    enabled: !!username,
  });

  if (isLoading) {
    return (
      <div className="container max-w-2xl py-8 space-y-4">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-48 w-full" />
      </div>
    );
  }

  if (!profile) {
    return <div className="container py-12 text-center text-muted-foreground">User not found</div>;
  }

  const s = profile.stats;

  return (
    <div className="container max-w-2xl py-8 space-y-6">
      <Card>
        <CardContent className="p-6 flex items-start gap-6">
          <Avatar className="h-20 w-20">
            <AvatarImage src={profile.avatarUrl || undefined} />
            <AvatarFallback className="text-xl">{profile.username[0].toUpperCase()}</AvatarFallback>
          </Avatar>
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold">{profile.displayName || profile.username}</h1>
              <Badge variant="outline">{profile.role}</Badge>
            </div>
            <p className="text-sm text-muted-foreground">@{profile.username}</p>
            {profile.bio && <p className="text-sm mt-2">{profile.bio}</p>}
            <p className="text-xs text-muted-foreground mt-2">Joined {formatRelativeTime(profile.joinedAt)}</p>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-2 gap-4">
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><FileText className="h-4 w-4" /> Posts</CardTitle></CardHeader>
          <CardContent><p className="text-2xl font-bold">{s.posts}</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><Lightbulb className="h-4 w-4" /> Suggestions</CardTitle></CardHeader>
          <CardContent><p className="text-2xl font-bold">{s.suggestions} <span className="text-sm font-normal text-muted-foreground">({s.acceptedSuggestions} accepted)</span></p></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><ThumbsUp className="h-4 w-4" /> Likes Received</CardTitle></CardHeader>
          <CardContent><p className="text-2xl font-bold">{s.likesReceived}</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><Trophy className="h-4 w-4" /> Reputation</CardTitle></CardHeader>
          <CardContent><p className="text-2xl font-bold">{s.reputation}</p></CardContent>
        </Card>
      </div>
    </div>
  );
}
